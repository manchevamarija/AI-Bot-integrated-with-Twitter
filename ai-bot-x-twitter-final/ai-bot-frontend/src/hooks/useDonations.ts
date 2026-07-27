import { useCallback, useEffect, useState } from 'react';
import type { CreateDonationBatchRequest, DonationBatchResponse } from '../api/types/donation.ts';
import donationApi from '../api/donationApi.ts';
import useSnackbar from './useSnackbar.ts';

const useDonations = () => {
  const { showSnackbar } = useSnackbar();
  const [donations, setDonations] = useState<DonationBatchResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setDonations((await donationApi.findAll()).data);
      setError(null);
    } catch {
      setError('Донациите не може да се вчитаат. Провери дали backend-от работи.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  const onCreate = async (data: CreateDonationBatchRequest) => {
    try {
      await donationApi.add(data);
      showSnackbar(`Креирана е донација со ${data.postIds.length} објави.`);
      await load();
    } catch (cause) {
      showSnackbar('Донацијата не може да се креира.', 'error');
      throw cause;
    }
  };

  const onApprove = async (id: number) => {
    try {
      await donationApi.approve(String(id));
      showSnackbar(`Донацијата #${id} е одобрена.`);
      await load();
    } catch (cause) {
      showSnackbar('Донацијата не може да се одобри.', 'error');
      throw cause;
    }
  };

  const onSubmit = async (id: number) => {
    try {
      await donationApi.submit(String(id));
      showSnackbar(`Донацијата #${id} е испратена до Везилка.`);
      await load();
    } catch (cause) {
      showSnackbar(
        'Испраќањето не успеа. Провери го API-клучот за Везилка и backend логот.',
        'error'
      );
      throw cause;
    }
  };

  return {
    donations,
    loading,
    error,
    reload: load,
    onCreate,
    onApprove,
    onSubmit,
  };
};

export default useDonations;
